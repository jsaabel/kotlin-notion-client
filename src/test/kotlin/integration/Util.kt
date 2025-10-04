package integration

fun integrationTestEnvVarsAreSet(vararg envVars: String = arrayOf("NOTION_API_TOKEN", "NOTION_TEST_PAGE_ID")): Boolean {
    envVars.forEach {
        if (System.getenv(it).isNullOrBlank()) {
            return false
        }
    }
    return true
}

fun shouldCleanupAfterTest(): Boolean = System.getenv("NOTION_CLEANUP_AFTER_TEST")?.lowercase() != "false"
