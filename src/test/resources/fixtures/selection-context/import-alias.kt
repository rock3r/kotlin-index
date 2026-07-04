import org.jetbrains.jewel.foundation.theme.SelectionContainer as SC

@Target(AnnotationTarget.FUNCTION)
annotation class Composable

@Composable
fun Panel() {
    SC {
        ActionButton()
    }
}

@Composable
fun ActionButton() {}
