@Target(AnnotationTarget.FUNCTION)
annotation class Composable

@Composable
fun Panel() {
    SelectionContainer {
        fun localRow() {
            ActionButton()
        }
        localRow()
    }
}

@Composable
fun ActionButton() {}
