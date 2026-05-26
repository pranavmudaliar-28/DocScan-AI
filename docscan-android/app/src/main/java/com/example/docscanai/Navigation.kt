package com.example.docscanai

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.docscanai.ui.main.MainScreen
import com.example.docscanai.ui.splash.SplashScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Splash)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Splash> {
          SplashScreen(onSplashComplete = {
            backStack[backStack.lastIndex] = Main
          })
        }
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) })
        }
      },
  )
}
