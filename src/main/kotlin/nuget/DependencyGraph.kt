package nuget

/** Граф, кратчайшие расстояния BFS и узлы, чьи зависимости не читались из-за глубины. */
data class DependencyGraph(
    val root: String,
    val edges: Map<String, List<String>>,
    val depths: Map<String, Int>,
    val boundary: Set<String>
)
