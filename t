[1mdiff --git a/app/src/main/java/com/michaeltroger/gruenerpass/MainActivity.kt b/app/src/main/java/com/michaeltroger/gruenerpass/MainActivity.kt[m
[1mindex 5ffbef3..4dfbba9 100644[m
[1m--- a/app/src/main/java/com/michaeltroger/gruenerpass/MainActivity.kt[m
[1m+++ b/app/src/main/java/com/michaeltroger/gruenerpass/MainActivity.kt[m
[36m@@ -58,7 +58,7 @@[m [mclass MainActivity : AppCompatActivity(R.layout.activity_main), AddFile {[m
 [m
     private val timeoutHandler: Handler = Handler(Looper.getMainLooper())[m
     private lateinit var interactionTimeoutRunnable: Runnable[m
[31m-    private lateinit var navController: NavController[m
[32m+[m[32m    private var navController: NavController? = null[m
     private val appBarConfiguration = AppBarConfiguration.Builder([m
         R.id.certificatesFragment,[m
         R.id.certificatesListFragment,[m
[36m@@ -82,19 +82,19 @@[m [mclass MainActivity : AppCompatActivity(R.layout.activity_main), AddFile {[m
         interactionTimeoutRunnable = InteractionTimeoutRunnable()[m
         startTimeoutHandler()[m
 [m
[31m-        lifecycleScope.launch {[m
[31m-            repeatOnLifecycle(Lifecycle.State.STARTED) {[m
[31m-                combine([m
[31m-                    lockedRepo.isAppLocked(),[m
[31m-                    showListLayout,[m
[31m-                    pdfImporter.hasPendingFile(),[m
[31m-                    navController.currentBackStackEntryFlow,[m
[31m-                    ::autoRedirect[m
[31m-                ).collect {[m
[31m-                    // do nothing[m
[31m-                }[m
[31m-            }[m
[31m-        }[m
[32m+[m[32m//        lifecycleScope.launch {[m
[32m+[m[32m//            repeatOnLifecycle(Lifecycle.State.STARTED) {[m
[32m+[m[32m//                combine([m
[32m+[m[32m//                    lockedRepo.isAppLocked(),[m
[32m+[m[32m//                    showListLayout,[m
[32m+[m[32m//                    pdfImporter.hasPendingFile(),[m
[32m+[m[32m//                    navController?.currentBackStackEntryFlow,[m
[32m+[m[32m//                    ::autoRedirect[m
[32m+[m[32m//                ).collect {[m
[32m+[m[32m//                    // do nothing[m
[32m+[m[32m//                }[m
[32m+[m[32m//            }[m
[32m+[m[32m//        }[m
     }[m
 [m
     private fun setUpNavigation() {[m
[36m@@ -105,10 +105,10 @@[m [mclass MainActivity : AppCompatActivity(R.layout.activity_main), AddFile {[m
             navController = navHostFragment.navController[m
 [m
             navGraph.setStartDestination(getStartDestinationUseCase())[m
[31m-            navController.graph = navGraph[m
[32m+[m[32m            navController!!.graph = navGraph[m
 [m
             setupActionBarWithNavController([m
[31m-                navController = navController,[m
[32m+[m[32m                navController = navController!!,[m
                 configuration = appBarConfiguration.build()[m
             )[m
         }[m
[36m@@ -161,7 +161,7 @@[m [mclass MainActivity : AppCompatActivity(R.layout.activity_main), AddFile {[m
     }[m
 [m
     override fun onSupportNavigateUp(): Boolean {[m
[31m-        return navController.navigateUp() || super.onSupportNavigateUp()[m
[32m+[m[32m        return navController?.navigateUp() == true || super.onSupportNavigateUp()[m
     }[m
 [m
     override fun onDestroy() {[m
