package com.monkey.wisdom

import android.app.Application
import com.monkey.wisdom.di.ServiceLocator

/**
 * 应用入口：初始化依赖容器与本地会话。
 */
class MonkeyWisdomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
