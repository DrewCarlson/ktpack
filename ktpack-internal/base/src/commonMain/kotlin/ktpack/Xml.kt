package ktpack

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML


@OptIn(ExperimentalXmlUtilApi::class)
val xml = XML {
    @Suppress("DEPRECATION")
    policy = DefaultXmlSerializationPolicy(
        pedantic = false,
        unknownChildHandler = { _, _, _, _, _ -> emptyList() },
    )
}
