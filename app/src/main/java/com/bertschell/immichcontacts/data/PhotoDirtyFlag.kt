package com.bertschell.immichcontacts.data

/** Set when a contact photo was changed; consumed by the contacts list to trigger one reload. */
object PhotoDirtyFlag {
    @Volatile
    var isDirty: Boolean = false
}
