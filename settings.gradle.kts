rootProject.name = "mercury-order-system"

include(
    ":common:common-events",
    ":services:api-gateway",
    ":services:inventory",
    ":services:orders",
    ":services:payments"
)