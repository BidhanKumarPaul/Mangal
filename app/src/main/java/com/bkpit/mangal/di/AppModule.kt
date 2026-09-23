package com.bkpit.mangal.di

import android.content.Context
import com.bkpit.mangal.tools.ToolRegistry
import com.bkpit.mangal.tools.permissions.PermissionManager
import com.bkpit.mangal.tools.impl.AlarmTool
import com.bkpit.mangal.tools.impl.CalendarEventTool
import com.bkpit.mangal.tools.impl.OpenAppTool
import com.bkpit.mangal.tools.impl.SendSmsTool
import com.bkpit.mangal.tools.impl.SettingsToggleTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager =
        PermissionManager(context)

    @Provides
    @Singleton
    fun provideToolRegistry(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager
    ): ToolRegistry {
        // Phase 4: the 5 tools required by spec, registered by name. Each tool
        // declares its own required permissions and is skipped (with a clear
        // reason returned to the LLM) if they aren't granted.
        val registry = ToolRegistry(permissionManager)
        registry.register(AlarmTool(context))
        registry.register(CalendarEventTool(context))
        registry.register(OpenAppTool(context))
        registry.register(SendSmsTool(context))
        registry.register(SettingsToggleTool(context))
        return registry
    }
}
