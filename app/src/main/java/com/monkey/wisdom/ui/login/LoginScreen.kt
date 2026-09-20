package com.monkey.wisdom.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.core.constants.UserType
import com.monkey.wisdom.data.model.UserInfo
import com.monkey.wisdom.ui.components.AppTextField
import com.monkey.wisdom.ui.components.FormErrorText
import com.monkey.wisdom.ui.components.PrimaryButton
import com.monkey.wisdom.ui.components.UserTypeSelector
import com.monkey.wisdom.ui.theme.PageGradientEnd
import com.monkey.wisdom.ui.theme.PageGradientStart
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 登录 / 注册页（对齐 Web 端 `views/login.vue` 的交互与视觉）。
 *
 * @param onLoginSuccess 登录成功回调，由组合根负责页面切换
 * @param hintMessage    进入页面时的提示文案（如会话过期提示），展示后通过 [onHintShown] 回调消费
 * @param onHintShown    提示已展示，用于清空外部状态避免重复弹出
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (UserInfo) -> Unit,
    modifier: Modifier = Modifier,
    hintMessage: String? = null,
    onHintShown: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.loggedInUser) {
        uiState.loggedInUser?.let { user ->
            viewModel.consumeLoggedInUser()
            onLoginSuccess(user)
        }
    }

    // 会话过期等外部提示：先展示再回调，避免状态清空导致提示被打断
    LaunchedEffect(hintMessage) {
        hintMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onHintShown()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            viewModel.consumeToastMessage()
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(PageGradientStart, PageGradientEnd))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                        Text(
                            text = uiState.title,
                            modifier = Modifier.fillMaxWidth(),
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.subtitle,
                            modifier = Modifier.fillMaxWidth(),
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        when (uiState.mode) {
                            AuthMode.LOGIN -> LoginFormSection(
                                state = uiState,
                                onMobileChange = viewModel::onLoginMobileChange,
                                onPasswordChange = viewModel::onLoginPasswordChange,
                            )

                            AuthMode.REGISTER -> RegisterFormSection(
                                state = uiState,
                                onUserTypeSelect = viewModel::onUserTypeSelect,
                                onRealNameChange = viewModel::onRealNameChange,
                                onMobileChange = viewModel::onRegisterMobileChange,
                                onPasswordChange = viewModel::onRegisterPasswordChange,
                                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        FormErrorText(message = uiState.errorMessage)
                        Spacer(modifier = Modifier.height(8.dp))

                        PrimaryButton(
                            text = uiState.submitText,
                            loading = uiState.loading,
                            onClick = {
                                when (uiState.mode) {
                                    AuthMode.LOGIN -> viewModel.submitLogin()
                                    AuthMode.REGISTER -> viewModel.submitRegister()
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        SwitchModeTip(
                            mode = uiState.mode,
                            enabled = !uiState.loading,
                            onSwitch = viewModel::switchMode,
                        )
                    }
                }
            }
        }
    }
}

/** 登录表单：账号 + 密码 */
@Composable
private fun LoginFormSection(
    state: LoginUiState,
    onMobileChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    AppTextField(
        value = state.loginMobile,
        onValueChange = onMobileChange,
        label = "账号",
        placeholder = "请输入手机号 / 账号",
        keyboardType = KeyboardType.Phone,
        imeAction = ImeAction.Next,
        enabled = !state.loading,
        maxLength = MAX_MOBILE_LENGTH,
    )
    Spacer(modifier = Modifier.height(16.dp))
    AppTextField(
        value = state.loginPassword,
        onValueChange = onPasswordChange,
        label = "密码",
        placeholder = "请输入密码",
        isPassword = true,
        imeAction = ImeAction.Done,
        enabled = !state.loading,
    )
}

/** 注册表单：用户类型 + 姓名 + 手机号 + 密码 + 确认密码 */
@Composable
private fun RegisterFormSection(
    state: LoginUiState,
    onUserTypeSelect: (Int) -> Unit,
    onRealNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
) {
    Text(text = "用户类型", color = TextPrimary, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(8.dp))
    UserTypeSelector(
        options = UserType.registerOptions,
        selectedValue = state.registerUserType,
        onSelect = onUserTypeSelect,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    AppTextField(
        value = state.registerRealName,
        onValueChange = onRealNameChange,
        label = "真实姓名",
        placeholder = "请输入真实姓名",
        enabled = !state.loading,
    )
    Spacer(modifier = Modifier.height(16.dp))
    AppTextField(
        value = state.registerMobile,
        onValueChange = onMobileChange,
        label = "手机号",
        placeholder = "请输入 11 位手机号",
        keyboardType = KeyboardType.Phone,
        enabled = !state.loading,
        maxLength = MAX_MOBILE_LENGTH,
    )
    Spacer(modifier = Modifier.height(16.dp))
    AppTextField(
        value = state.registerPassword,
        onValueChange = onPasswordChange,
        label = "密码",
        placeholder = "请输入密码（至少 6 位）",
        isPassword = true,
        enabled = !state.loading,
    )
    Spacer(modifier = Modifier.height(16.dp))
    AppTextField(
        value = state.registerConfirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = "确认密码",
        placeholder = "请再次输入密码",
        isPassword = true,
        imeAction = ImeAction.Done,
        enabled = !state.loading,
    )
}

/** 底部模式切换提示 */
@Composable
private fun SwitchModeTip(
    mode: AuthMode,
    enabled: Boolean,
    onSwitch: (AuthMode) -> Unit,
) {
    val isLogin = mode == AuthMode.LOGIN
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isLogin) "还没有账号？" else "已有账号？",
            color = TextSecondary,
            fontSize = 14.sp,
        )
        Text(
            text = if (isLogin) "立即注册" else "去登录",
            modifier = Modifier
                .clickable(enabled = enabled) {
                    onSwitch(if (isLogin) AuthMode.REGISTER else AuthMode.LOGIN)
                }
                .padding(start = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 手机号固定 11 位 */
private const val MAX_MOBILE_LENGTH = 11
