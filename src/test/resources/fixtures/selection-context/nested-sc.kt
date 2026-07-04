@Target(AnnotationTarget.FUNCTION)
annotation class Composable

@Composable
fun UserPromptText() {
    SelectionContainer {
        SelectionContainer {
            ActionButton()
        }
    }
}

@Composable
fun ActionButton() {}
