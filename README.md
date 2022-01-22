# State 패턴 4가지 전략

이 포스트는 ViewModel에서 데이터를 요청하고 결과를 UI로 만들고는 합니다.

그때마다 데이터 요청을 할 때 구글 앱 아키텍쳐에서는 State 패턴을 권장합니다.

이를 위해 네가지의 전략을 학습하고자 하여 이 포스팅을 합니다.

UI 모델링을 처리하는 방법은 정말 많지만 일반적으로 사용되는 4가지 모델링 패턴으로 얘기를 해보도록 하겠습니다.

## 목차
1. 여러 개의 State를 만들고 Loading, Error 별도로 만들기
2. sealed class로 하나의 State를 만들고 상태 개수만큼 구현체 만들기
3. 하나의 State data class를 만들어서 관리하기
4. 하나의 State data class와 Loading, Error 별도로 만들기

## 요구 사항
“유저 정보를 받아와 2초정도 후에 이름과 나이를 화면에 그려주세요. 오류가 발생하면 따로 표시 해 주세요”

<div align="center">
  <img src = "https://user-images.githubusercontent.com/48902047/150623803-35d945f6-8e1d-476c-bff6-92007dbbf3e1.gif" width="30%" height="30%">
</div>

## State 1

여러개의 State를 만들고 Loading, Error 별도로 만들기
 ```Kotlin
@HiltViewModel
class State1ViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    private val _name: MutableStateFlow<String> = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _age: MutableStateFlow<String> = MutableStateFlow("")
    val age: StateFlow<String> = _age.asStateFlow()

    init {
        viewModelScope.launch {
            _loading.value = true
            val result = userRepository.getUser()
            result
                .onSuccess { user ->
                    _loading.value = false
                    _error.value = false
                    _name.value = user.name
                    _age.value = user.age.toString()
                }
                .onFailure {
                    _loading.value = false
                    _error.value = true
                }
        }
    }
}
```
 ```Kotlin
@AndroidEntryPoint
class State1Activity : StateActivity() {

    private val viewModel: State1ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.name.collect { name ->
                binding.name.text = name
            }
        }

        lifecycleScope.launch {
            viewModel.age.collect { age ->
                binding.age.text = age
            }
        }

        lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                binding.loading.isVisible = loading
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                binding.error.isVisible = error
            }
        }

        lifecycleScope.launch {
            combine(viewModel.loading, viewModel.error) { loading, error -> !loading && !error }
                .collect { isVisible ->
                    binding.card.isVisible = isVisible
                }
        }
    }
}
```
#### 장점
+ 개별적으로 State를 정의함으로써 원하는 데이터만 변경이 가능하다.
+ 각 State가 서로에게 영향을 발생시키지 않는다.
+ 데이터바인딩 코드를 작성하기 쉽다
#### 단점
+ State 가짓수가 많아 실수를 유발하기 쉽다.
+ Event를 통해 상태가 어떻게 변경될지 예측이 어렵다.
+ 구독을 처리하는 코드가 많고 복잡하다.
#### 사용하기 좋은 곳
+ Base 구조를 사용하지 않는 간단한 화면에 적합
+ State와 데이터바인딩을 1:1 구조로 사용하는 경우

## State 2
sealed class로 하나의 State를 만들고 상태 개수만큼 구현체 만들기
 ```Kotlin
@HiltViewModel
class State2ViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    sealed class State {

        data class Success(
            val name: String,
            val age: String
        ) : State()

        object Empty : State()

        object Failure : State()

        object Loading : State()
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()


    init {
        viewModelScope.launch {
            _state.value = State.Loading
            val result = userRepository.getUser()
            result
                .onSuccess { user ->
                    val (name, age) = (user.name to user.age.toString())
                    if (name.isEmpty() && age.isEmpty()) {
                        _state.value = State.Empty
                    } else {
                        _state.value = State.Success(user.name, user.age.toString())
                    }
                }
                .onFailure {
                    _state.value = State.Failure
                }
        }
    }
}
```
 ```Kotlin
@AndroidEntryPoint
class State2Activity : StateActivity() {

    private val viewModel: State2ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.loading.isVisible = state is State.Loading
                binding.error.isVisible = state is State.Failure
                binding.card.isVisible = state is State.Success

                if (state is State.Success) {
                    binding.name.text = state.name
                    binding.age.text = state.age
                }
            }
        }
    }
}
```
#### 장점
+ MVI와 유사한 형태의 구현으로 UI에 대한 의도를 명확하게 표현이 가능
+ sealed class를 활용함으로써 객체지향적인 처리에 유리하다.
+ UI를 변경하는 코드가 분산되지 않아 코드 분석이 편하다.
+ 한 가지 상태만 가지기 때문에 상태가 섞이는 것을 고려하지 않아도 된다.
#### 단점
+ 표현하고자 하는 모든 상태를 나열해야 하기 때문에 화면이 복잡해지면 상태가 비약적으로 늘어나고 부분적인 업데이트가 불가하다.
+ 각 상태가 변경되면 이전 상태를 별도로 보관하지 않는 한 이전 상태에 대한 데이터를 복구할 방법이 없다.
+ 공통으로 쓰이는 상태를 표현하기가 어렵고 공유하는 것은 더욱 복잡함
+ 데이터바인딩을 사용하기가 상당히 어렵다.
#### 사용하기 좋은곳
+ 명확한 의도를 갖고 UI를 표현하는 경우 (생각을 바로 상태로 변경 가능)
+ 로딩이나 오류를 전체 상태로 취급할 수 있는 경우
+ 부분적인 데이터 수정 혹은 이전 상태가 필요없는 경우

## State 3
하나의 State data class를 만들어서 관리하기
 ```Kotlin
@HiltViewModel
class State3ViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val error: Boolean = false,
        val name: String = "",
        val age: String = ""
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = userRepository.getUser()
            _state.update { state -> state.copy(loading = true) }

            result
                .onSuccess { user ->
                    _state.update { state ->
                        state.copy(
                            loading = false,
                            name = user.name,
                            age = user.age.toString()
                        )
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(
                            loading = false,
                            error = true
                        )
                    }
                }
        }
    }
}
```
 ```Kotlin
@AndroidEntryPoint
class State3Activity : StateActivity() {

    private val viewModel: State3ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.loading.isVisible = state.loading
                binding.error.isVisible = state.error
                binding.card.isVisible = !state.loading
                binding.name.text = state.name
                binding.age.text = state.age
            }
        }
    }
}
```
#### 장점
+ 하나의 데이터에 필요한 모든 상태를 포함할 수 있어 UI에 대한 비즈니스 로직 처리가 편하다.
+ data class를 사용함으로써 copy라는 강력한 부분 업데이트 수단을 제공
+ UI를 변경하는 코드가 분산되지 않아 코드 분석이 편하다.
+ LiveData나 StateFlow의 map 함수와 distinctUntilChanged를 조합해 원하는 형태로 데이터바인딩에 적용 가능
#### 단점
+ copy가 어느정도 안전성을 보장하지만 데이터 변경에 대한 동시성 이슈에 취약하다.
+ 화면이 복잡해지는 경우 data class에 필요한 프로퍼티가 비약적으로 늘어난다.
+ data class 특성상 하나의 프로퍼티 값만 바뀌어도 구독하고 있는 모든 옵저버에 변경을 알려 애니메이션같은 1회성 동작에 주의가 필요하다.
#### 사용하기 좋은 곳
+ UI에 대한 부분적인 수정이 빈번하게 일어나는 경우
+ UI 비즈니스 로직이 다소 복잡한 경우
+ 상태를 조합하여 자주 사용하는 경우
+ 로딩이나 오류도 상태에 포함하여 취급할 수 있는 경우

## State 4
하나의 State data class와 Loading, Error 별도로 만들기
 ```Kotlin
@HiltViewModel
class State4ViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    data class State(
        val name: String = "",
        val age: String = ""
    ) {
        val isEmpty: Boolean = name.isEmpty() && age.isEmpty()
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            _loading.value = true
            val result = userRepository.getUser()

            result
                .onSuccess { user ->
                    _loading.value = false
                    _state.update { state ->
                        state.copy(
                            name = user.name,
                            age = user.age.toString()
                        )
                    }
                }
                .onFailure {
                    _loading.value = false
                    _error.value = true
                }
        }
    }
}
```
 ```Kotlin
@AndroidEntryPoint
class State4Activity : StateActivity() {

    private val viewModel: State4ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.card.isVisible = !state.isEmpty
                binding.name.text = state.name
                binding.age.text = state.age
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                binding.error.isVisible = error
            }
        }

        lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                binding.loading.isVisible = loading
            }
        }
    }
}
```
#### 장점
+ UI 상태가 로딩이나 오류에 의존적이지 않아 따로 처리가 가능하다.
+ Base를 사용하는 경우 확장성 있는 구조를 제공한다.
#### 단점
+ UI 상태가 로딩이나 오류와 의존성이 있는 경우 처리가 다소 복잡하다.
+ 로딩 상태를 여러 곳에서 변경할 수 있기 때문에 상태 변경에 주의가 필요하다.
#### 사용하기 좋은 곳
+ UI 상태를 로딩과 오류와 나누어서 사용하는 경우
+ Base에서 로딩과 오류를 일반적으로 처리하고 싶은 경우

### 읽어보기
https://lordraydenmk.github.io/2021/modelling-ui-state/
https://medium.com/@laco2951/android-ui-state-modeling-%EC%96%B4%EB%96%A4%EA%B2%8C-%EC%A2%8B%EC%9D%84%EA%B9%8C-7b6232543f25
