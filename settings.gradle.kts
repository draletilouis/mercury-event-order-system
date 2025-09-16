rootProject.name = "mercury-order-system"

include(
    ":common:common-events",
    ":common:common-tracing",
    ":services:api-gateway",
    ":services:inventory",
    ":services:orders",
    ":services:payments"
)