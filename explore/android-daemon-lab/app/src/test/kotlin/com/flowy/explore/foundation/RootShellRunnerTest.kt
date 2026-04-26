package com.flowy.explore.foundation

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream

class RootShellRunnerTest {
  @Test
  fun run_skipsMissingBinaryCandidate() {
    val runner = RootShellRunner(
      candidates = listOf("/missing/su", "su"),
      processFactory = { command ->
        if (command.first() == "/missing/su") {
          throw IOException("Cannot run program \"/missing/su\": error=2, No such file or directory")
        }
        fakeProcess(stdout = "root-ok".toByteArray(), exitCode = 0)
      },
    )

    val result = runner.run("id")

    assertEquals(0, result.exitCode)
    assertEquals("root-ok", result.stdoutText())
  }

  @Test(expected = IllegalStateException::class)
  fun run_preservesNonMissingFailure() {
    val runner = RootShellRunner(
      candidates = listOf("su"),
      processFactory = {
        throw SecurityException("permission denied")
      },
    )

    runner.run("id")
  }

  @Test
  fun run_collectsLargeStdoutBeforeWaitCompletes() {
    val png = ByteArray(256 * 1024) { (it % 251).toByte() }
    val runner = RootShellRunner(
      candidates = listOf("su"),
      processFactory = {
        streamingProcess(png)
      },
    )

    val result = runner.run("screencap", timeoutMs = 1_000)

    assertArrayEquals(png, result.stdoutBytes)
  }

  private fun fakeProcess(stdout: ByteArray, exitCode: Int): Process {
    return object : Process() {
      override fun getInputStream() = ByteArrayInputStream(stdout)
      override fun getErrorStream() = ByteArrayInputStream(byteArrayOf())
      override fun getOutputStream() = PipedOutputStream()
      override fun waitFor() = exitCode
      override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit) = true
      override fun exitValue() = exitCode
      override fun destroy() = Unit
      override fun destroyForcibly(): Process = this
      override fun isAlive() = false
    }
  }

  private fun streamingProcess(stdout: ByteArray): Process {
    val input = PipedInputStream(64)
    val output = PipedOutputStream(input)
    val writer = Thread {
      output.use { stream ->
        stdout.forEach { byte -> stream.write(byte.toInt()) }
      }
    }.apply { start() }
    return object : Process() {
      override fun getInputStream() = input
      override fun getErrorStream() = ByteArrayInputStream(byteArrayOf())
      override fun getOutputStream() = PipedOutputStream()
      override fun waitFor() = 0
      override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean {
        writer.join(unit.toMillis(timeout))
        return !writer.isAlive
      }

      override fun exitValue() = 0
      override fun destroy() = Unit
      override fun destroyForcibly(): Process = this
      override fun isAlive() = writer.isAlive
    }
  }
}
