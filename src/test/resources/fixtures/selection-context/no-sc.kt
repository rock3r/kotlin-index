@Target(AnnotationTarget.FUNCTION)
annotation class Composable

@Composable
fun Panel() {
    ActionButton()
}

@Composable
fun ActionButton() {}
