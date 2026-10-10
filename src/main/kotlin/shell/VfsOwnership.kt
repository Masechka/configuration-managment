package shell

/** Виртуальные владелец и группа; не связаны с учётными записями или правами macOS. */
data class VfsOwnership(val owner: String = "root", val group: String = "root")
