package com.example.exchange

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import org.iesra.revilofe.ExchangeRateProvider
import org.iesra.revilofe.ExchangeService
import org.iesra.revilofe.InMemoryExchangeRateProvider
import org.iesra.revilofe.Money

class ExchangeServiceDesignedBatteryTest : DescribeSpec({

    afterTest {
        clearAllMocks()
    }

    describe("A. Validación de entrada") { // Tests del 1 al 4
        it("Debe lanzar excepción si la cantidad es 0"){
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException> {
                service.exchange(Money(0, "USD"), "EUR")
            }
        }

        it("Debe lanzar excepción si la cantidad es negativa."){
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException>{
                service.exchange(Money(-4,"EUR"),"USD")
            }
        }

        it("Debe lanzar excepción si la moneda origen no tiene 3 letras."){
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException>{
                service.exchange(Money(5,"EURO"),"USD")
            }
        }

        it("Debe lanzar excepción si la moneda destino no tiene 3 letras."){
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException>{
                service.exchange(Money(5,"EUR"),"DOLAR")
            }
        }
    }

    describe("B. Relación entre moneda origen y destino") { // Test 5

    }

    describe("C. Estrategia de búsqueda de tasas") { // Tests del 6 al 11

    }



}
