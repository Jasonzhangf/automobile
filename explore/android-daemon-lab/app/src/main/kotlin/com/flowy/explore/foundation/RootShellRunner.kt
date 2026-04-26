package com.flowy.explore.foundation

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class RootShellRunner(
  private val candidates: List<String> = defaultCandidates(),
  private val processFactory: (List<String>) -> Process = {
    ProcessBuilder(it).redirectErrorStream(true).start()
  },
) {
  fun run(command: String, timeoutMs: Long = 5000): Result {
    var lastFailure: Throwable? = null
    candidates.forEach { candidate ->
      try {
        return launch(candidate, command, timeoutMs)
      } catch (throwable: Throwable) {
        if (throwable.isMissingBinary()) {
          lastFailure = throwable
        } else {
          throw throwable
        }
      }
    }
    throw IllegalStateException("ROOT_BINARY_NOT_FOUND", lastFailure)
  }

  private fun launch(binary: String, command: String, timeoutMs: Long): Result {
    val process = try {
      processFactory(listOf(binary, "-c", command))
    } catch (throwable: Throwable) {
      if (throwable.isMissingBinary()) {
        throw throwable
      }
      throw IllegalStateException("ROOT_PROCESS_START_FAILED", throwable)
    }
    val stdoutCollector = StreamCollector(process.inputStream)
    stdoutCollector.start()
    val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
    if (!finished) {
      process.destroyForcibly()
      stdoutCollector.join(timeoutMs)
      error("ROOT_COMMAND_TIMEOUT")
    }
    stdoutCollector.join(timeoutMs)
    val stdoutBytes = stdoutCollector.resultOrThrow()
    return Result(process.exitValue(), stdoutBytes)
  }

  private fun Throwable.isMissingBinary(): Boolean {
    return this is IOException &&
      (message?.contains("No such file", ignoreCase = true) == true ||
        message?.contains("error=2") == true)
  }

  private class StreamCollector(inputStream: java.io.InputStream) : Thread("flowy-root-stdout") {
    private val source = inputStream
    @Volatile private var bytes: ByteArray? = null
    @Volatile private var failure: Throwable? = null

    override fun run() {
      try {
        source.use { bytes = it.readBytes() }
      } catch (throwable: Throwable) {
        failure = throwable
      }
    }

    fun resultOrThrow(): ByteArray {
      failure?.let { throw IllegalStateException("ROOT_STDOUT_READ_FAILED", it) }
      return bytes ?: byteArrayOf()
    }
  }

  data class Result(
    val exitCode: Int,
    val stdoutBytes: ByteArray,
  ) {
    fun stdoutText(): String = stdoutBytes.toString(Charsets.UTF_8).trim()
  }

  companion object {
    fun defaultCandidates(): List<String> {
      return listOf("su", "/system/xbin/su", "/system/bin/su", "/sbin/su", "/su/bin/su")
        .distinct()
        .filter { it == "su" || File(it).exists() }
    }
  }
}
