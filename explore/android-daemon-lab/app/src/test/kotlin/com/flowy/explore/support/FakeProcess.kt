package com.flowy.explore.support

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FakeProcess(
  private val stdout: ByteArray = byteArrayOf(),
  private val stderr: ByteArray = byteArrayOf(),
  private val exitCode: Int = 0,
) : Process() {
  override fun getInputStream() = ByteArrayInputStream(stdout)

  override fun getErrorStream() = ByteArrayInputStream(stderr)

  override fun getOutputStream() = ByteArrayOutputStream()

  override fun waitFor() = exitCode

  override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit) = true

  override fun exitValue() = exitCode

  override fun destroy() = Unit

  override fun destroyForcibly(): Process = this

  override fun isAlive() = false
}
