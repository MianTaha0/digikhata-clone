package com.digikhata.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 3b.2: lazy Firestore accessor.
 *
 * We don't provide FirebaseFirestore as a Hilt @Singleton directly because the
 * default FirebaseApp requires google-services.json to be present at install
 * time. The plugin is still commented out in app/build.gradle.kts, and this
 * indirection keeps the graph constructible on CI / fresh clones. Firestore
 * will throw on `.getInstance()` if Firebase isn't initialized; PushWorker
 * catches that and returns Result.retry().
 */
@Singleton
class FirestoreProvider @Inject constructor() {
    fun get(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
