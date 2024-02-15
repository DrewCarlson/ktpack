package ktpack

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig

val toml = Toml(
    inputConfig = TomlInputConfig(
        ignoreUnknownNames = true,
        allowEmptyValues = true,
        allowNullValues = true,
        allowEscapedQuotesInLiteralStrings = true,
        allowEmptyToml = true,
    ),
)
