package com.bertschell.immichcontacts.data

/** Set when a contact photo was changed; consumed by the contacts list for ONE targeted refresh. */
object PhotoDirtyFlag {
    @Volatile
    var isDirty: Boolean = false
    @Volatile
    var dirtyContactId: Long = -1L
}
