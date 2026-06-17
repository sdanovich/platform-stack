// Root build so platform-stack is independently buildable and publishable
// (`./gradlew publish` / `publishToMavenLocal`). Consuming projects no longer
// include these as source subprojects — they depend on the published artifacts.
rootProject.name = "platform-stack"

include(":backend-auth")
include(":android-auth")
