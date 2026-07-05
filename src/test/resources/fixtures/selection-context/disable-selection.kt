@Target(AnnotationTarget.FUNCTION)
annotation class Composable

@Composable
fun Panel() {
    SelectionContainer {
        DisableSelection {
            ActionButton()
        }
    }
}

@Composable
fun ActionButton() {}
