package com.flowy.explore.flows

import android.content.Context
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.CheckUpgradeBlock
import com.flowy.explore.blocks.DownloadUpgradeApkBlock
import com.flowy.explore.blocks.PromptInstallApkBlock
import com.flowy.explore.foundation.DevServerReader
import com.flowy.explore.foundation.UpgradeStateStore
import com.flowy.explore.foundation.VersionComparator
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.runtime.LocalLogStore
import kotlin.concurrent.thread

class UpgradeCheckFlow(private val context: Context) {
  private val appendLogBlock = AppendLogBlock(LocalLogStore(context))
  private val versionReader = VersionReader(context)
  private val upgradeStateStore = UpgradeStateStore(context)
  private val checkUpgradeBlock = CheckUpgradeBlock()
  private val downloadUpgradeApkBlock = DownloadUpgradeApkBlock(context)
  private val promptInstallApkBlock = PromptInstallApkBlock(context)

  fun check(onStatus: (String) -> Unit, onAvailable: (String) -> Unit = {}) {
    thread(name = "flowy-upgrade-check") {
      runCatching {
        onStatus("checking")
        appendLogBlock.info("upgrade_check_started", "checking upgrade")
        val config = DevServerReader(context).read()
        val currentVersion = versionReader.versionName()
        val result = checkUpgradeBlock.run(config, currentVersion)
        if (!result.available || !VersionComparator.isNewer(result.latestVersion, currentVersion)) {
          upgradeStateStore.clear()
          onStatus("already-latest")
          appendLogBlock.info("upgrade_not_available", "already latest: $currentVersion")
          return@thread
        }
        upgradeStateStore.savePending(result.latestVersion, result.manifestUrl)
        appendLogBlock.info("upgrade_available", "upgrade available ${result.latestVersion}")
        onAvailable(result.latestVersion)
        onStatus("upgrade-available ${result.latestVersion}")
      }.onFailure { throwable ->
        appendLogBlock.error("upgrade_check_failed", throwable.message ?: "upgrade failed")
        onStatus("upgrade-failed")
      }
    }
  }

  fun installPending(onStatus: (String) -> Unit) {
    thread(name = "flowy-upgrade-install") {
      runCatching {
        val version = upgradeStateStore.pendingVersion() ?: error("NO_PENDING_UPGRADE")
        val manifestUrl = upgradeStateStore.pendingManifestUrl() ?: error("NO_PENDING_UPGRADE")
        onStatus("downloading $version")
        appendLogBlock.info("upgrade_download_started", "downloading $version")
        val apkFile = downloadUpgradeApkBlock.run(manifestUrl)
        val installState = promptInstallApkBlock.run(apkFile)
        appendLogBlock.info("upgrade_install_prompted", installState)
        if (installState == "installer-opened") upgradeStateStore.clear()
        onStatus(if (installState == "installer-opened") "installer-opened $version" else "grant-install-permission")
      }.onFailure { throwable ->
        appendLogBlock.error("upgrade_install_failed", throwable.message ?: "install failed")
        onStatus("upgrade-failed")
      }
    }
  }
}
