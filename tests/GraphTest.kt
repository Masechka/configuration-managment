package nuget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/** Проверяет рекурсивный BFS, глубину, циклы и общий дочерний узел. */
class GraphTest {
    /** Уровни посещаются по ширине; цикл сохраняет ребро без повторного чтения. */
    @Test fun breadthFirstWithCycles() {
        val calls = mutableListOf<String>()
        val source = FileRepository.fromText("A: B C\nB: D\nC: D\nD: A")
        val repository = Repository { name -> calls += name; source.dependencies(name) }
        val graph = GraphBuilder(repository).build("A", 4)
        assertEquals(listOf("A", "B", "C", "D"), calls)
        assertEquals(mapOf("A" to 0, "B" to 1, "C" to 1, "D" to 2), graph.depths)
        assertEquals(listOf("D"), graph.edges["C"])
        assertEquals(listOf("A"), graph.edges["D"])
        assertTrue(AsciiTree.render(graph).any { "[seen]" in it })
    }

    /** Граничный уровень виден, но его зависимости не запрашиваются. */
    @Test fun respectsDepth() {
        val calls = mutableListOf<String>()
        val repository = Repository { name -> calls += name; listOf(Dependency("${name}A")) }
        val zero = GraphBuilder(repository).build("A", 0)
        assertEquals(mapOf("A" to emptyList()), zero.edges)
        assertTrue(calls.isEmpty())
        val graph = GraphBuilder(repository).build("A", 2)
        assertEquals(listOf("A", "AA"), calls)
        assertEquals(setOf("AAA"), graph.boundary)
        assertTrue(AsciiTree.render(graph).last().endsWith("[depth-limit]"))
        assertFailsWith<IllegalArgumentException> { GraphBuilder(repository).build("A", -1) }
    }

    /** Повторные рёбра и разный регистр NuGet не создают разные узлы. */
    @Test fun canonicalizesNames() {
        val repository = Repository { name ->
            if (name == "Demo") listOf(Dependency("CHILD"), Dependency("child"))
            else listOf(Dependency("demo"))
        }
        val graph = GraphBuilder(repository).build("Demo", 3)
        assertEquals(listOf("CHILD"), graph.edges["Demo"])
        assertEquals(listOf("Demo"), graph.edges["CHILD"])
        assertEquals(2, graph.edges.size)
    }

    /** tailrec не расходует стек даже при нескольких тысячах уровней. */
    @Test fun supportsDeepChain() {
        val repository = Repository { name -> listOf(Dependency("N${name.drop(1).toInt() + 1}")) }
        val graph = GraphBuilder(repository).build("N0", 3000)
        assertEquals(3001, graph.edges.size)
        assertEquals(setOf("N3000"), graph.boundary)
    }

    /** Дерево строится детерминированно, общий узел помечается повтором. */
    @Test fun rendersDiamond() {
        val repository = FileRepository.fromText("A: B C\nB: D\nC: D\nD:")
        val graph = GraphBuilder(repository).build("A", 4)
        assertEquals(listOf("A", "|-- B", "|   `-- D", "`-- C", "    `-- D [seen]"),
            AsciiTree.render(graph))
        assertEquals("A -> B, C", GraphOutput.lines(graph).first())
    }
}
