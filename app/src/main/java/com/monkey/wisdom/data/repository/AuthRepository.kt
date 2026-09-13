package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.UserInfo
import com.monkey.wisdom.data.model.request.RegisterRequest

/**
 * 认证数据仓库：屏蔽网络细节，向 ViewModel 提供领域结果。
 */
interface AuthRepository {

    /**
     * 登录
     *
     * @return 成功时携带会话用户信息（token 可能为空，与 Web 端一致：不阻塞登录流程）
     */
    suspend fun login(mobile: String, password: String): AppResult<UserInfo>

    /** 注册 */
    suspend fun register(request: RegisterRequest): AppResult<Unit>
}
