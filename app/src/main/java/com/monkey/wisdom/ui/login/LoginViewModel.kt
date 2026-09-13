package com.monkey.wisdom.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.storage.UserSession
import com.monkey.wisdom.core.util.Validators
import com.monkey.wisdom.data.model.request.RegisterRequest
import com.monkey.wisdom.data.repository.AuthRepository
import com.monkey.wisdom.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录 / 注册页 ViewModel：承载表单状态、校验、请求编排与一次性事件。
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ==== 表单输入 ====

    fun onLoginMobileChange(value: String) = updateForm { it.copy(loginMobile = value) }

    fun onLoginPasswordChange(value: String) = updateForm { it.copy(loginPassword = value) }

    fun onUserTypeSelect(userType: Int) = updateForm { it.copy(registerUserType = userType) }

    fun onRealNameChange(value: String) = updateForm { it.copy(registerRealName = value) }

    fun onRegisterMobileChange(value: String) = updateForm { it.copy(registerMobile = value) }

    fun onRegisterPasswordChange(value: String) = updateForm { it.copy(registerPassword = value) }

    fun onConfirmPasswordChange(value: String) = updateForm { it.copy(registerConfirmPassword = value) }

    /** 切换登录 / 注册模式，并清空历史提示 */
    fun switchMode(mode: AuthMode) = _uiState.update {
        it.copy(mode = mode, errorMessage = null, toastMessage = null)
    }

    // ==== 提交动作 ====

    /** 提交登录 */
    fun submitLogin() {
        val state = _uiState.value
        if (state.loading) return

        Validators.validateLogin(state.loginMobile.trim(), state.loginPassword)?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            when (val result = authRepository.login(state.loginMobile.trim(), state.loginPassword)) {
                is AppResult.Success -> {
                    // 登录态落本地，后续请求由拦截器自动携带 token
                    UserSession.save(result.data)
                    _uiState.update { it.copy(loading = false, loggedInUser = result.data) }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** 提交注册 */
    fun submitRegister() {
        val state = _uiState.value
        if (state.loading) return

        Validators.validateRegister(
            realName = state.registerRealName.trim(),
            mobile = state.registerMobile.trim(),
            password = state.registerPassword,
            confirmPassword = state.registerConfirmPassword,
        )?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
            return
        }

        val request = RegisterRequest(
            userType = state.registerUserType,
            realName = state.registerRealName.trim(),
            mobile = state.registerMobile.trim(),
            password = state.registerPassword,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            when (val result = authRepository.register(request)) {
                is AppResult.Success -> _uiState.update {
                    // 注册成功：切回登录表单并回填手机号（与 Web 端交互保持一致）
                    it.copy(
                        loading = false,
                        mode = AuthMode.LOGIN,
                        loginMobile = request.mobile,
                        loginPassword = "",
                        toastMessage = "注册成功，请登录",
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** 登录成功事件已消费 */
    fun consumeLoggedInUser() = _uiState.update { it.copy(loggedInUser = null) }

    /** 一次性提示已消费 */
    fun consumeToastMessage() = _uiState.update { it.copy(toastMessage = null) }

    /** 输入变更时清空错误提示，避免旧错误残留 */
    private fun updateForm(transform: (LoginUiState) -> LoginUiState) =
        _uiState.update { transform(it).copy(errorMessage = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(ServiceLocator.authRepository) }
        }
    }
}
