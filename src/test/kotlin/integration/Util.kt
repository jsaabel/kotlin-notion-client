package integration

/**
 * Checks if required environment variables for integration tests are set.
 *
 * @param envVars The environment variables to check. Defaults to ["NOTION_API_TOKEN", "NOTION_TEST_PAGE_ID"].
 * @return True if NOTION_RUN_INTEGRATION_TESTS is "true" and all specified envVars have non-blank values, false otherwise.
 */
fun integrationTestEnvVarsAreSet(vararg envVars: String = arrayOf("NOTION_API_TOKEN", "NOTION_TEST_PAGE_ID")): Boolean {
    if ((System.getenv("NOTION_RUN_INTEGRATION_TESTS")?.lowercase() ?: "false") != "true") {
        return false
    }
    return envVars.all { System.getenv(it).isNullOrBlank().not() }
}

fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"
