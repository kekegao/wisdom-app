package com.monkey.wisdom.di

import android.content.Context
import com.monkey.wisdom.core.network.NetworkModule
import com.monkey.wisdom.core.storage.UserSession
import com.monkey.wisdom.data.remote.api.AccountApi
import com.monkey.wisdom.data.remote.api.AppVersionApi
import com.monkey.wisdom.data.remote.api.AuthApi
import com.monkey.wisdom.data.remote.api.OrderApi
import com.monkey.wisdom.data.repository.AccountRepository
import com.monkey.wisdom.data.repository.AccountRepositoryImpl
import com.monkey.wisdom.data.repository.AppVersionRepository
import com.monkey.wisdom.data.repository.AppVersionRepositoryImpl
import com.monkey.wisdom.data.repository.AuthRepository
import com.monkey.wisdom.data.repository.AuthRepositoryImpl
import com.monkey.wisdom.data.repository.BankCardRepository
import com.monkey.wisdom.data.repository.BankCardRepositoryImpl
import com.monkey.wisdom.data.repository.LocalAccountStore
import com.monkey.wisdom.data.repository.LocalAccountStoreImpl
import com.monkey.wisdom.data.repository.OrderRepository
import com.monkey.wisdom.data.repository.OrderRepositoryImpl

/**
 * 轻量依赖容器（Service Locator）。
 *
 * 选型说明：工程页面规模有限，使用手写容器可避免引入注解处理器（Hilt/KSP），
 * 保证工程开箱即编译运行；后续模块增多时可平滑替换为 Hilt。
 */
object ServiceLocator {

    private lateinit var appContext: Context

    private val authApi: AuthApi by lazy { NetworkModule.createApi<AuthApi>() }
    private val orderApi: OrderApi by lazy { NetworkModule.createApi<OrderApi>() }
    private val accountApi: AccountApi by lazy { NetworkModule.createApi<AccountApi>() }
    private val appVersionApi: AppVersionApi by lazy { NetworkModule.createApi<AppVersionApi>() }

    /** 认证仓库 */
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authApi) }

    /** 运单仓库 */
    val orderRepository: OrderRepository by lazy { OrderRepositoryImpl(orderApi) }

    /** 智运宝账户仓库 */
    val accountRepository: AccountRepository by lazy { AccountRepositoryImpl(accountApi) }

    /** APP 版本仓库（检查更新） */
    val appVersionRepository: AppVersionRepository by lazy { AppVersionRepositoryImpl(appVersionApi) }

    /** 银行卡仓库（本地存储） */
    val bankCardRepository: BankCardRepository by lazy { BankCardRepositoryImpl(appContext) }

    /** 账户本地存储（提现冻结记录） */
    val localAccountStore: LocalAccountStore by lazy { LocalAccountStoreImpl(appContext) }

    /** 在 Application.onCreate 中调用，注入全局 Context 并初始化本地会话 */
    fun init(context: Context) {
        appContext = context.applicationContext
        UserSession.init(context)
    }
}
