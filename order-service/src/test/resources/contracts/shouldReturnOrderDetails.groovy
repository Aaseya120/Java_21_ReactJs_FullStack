package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return valid Order JSON contract for API Gateway / React UI consumer"
    request {
        method 'GET'
        url '/api/v1/orders/100'
        headers {
            header('Authorization', 'Bearer sample.jwt.token')
        }
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
            id: 100,
            userId: 1,
            productId: 10,
            quantity: 2,
            totalPrice: 99.99,
            status: "PENDING"
        )
    }
}
