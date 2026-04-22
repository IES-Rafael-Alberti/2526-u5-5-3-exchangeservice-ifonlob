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

    describe("A. Validación de entrada") {
        it("Debe lanzar excepción si la cantidad es 0") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException> {
                service.exchange(Money(0, "USD"), "EUR")
            }
        }

        it("Debe lanzar excepción si la cantidad es negativa.") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException> {
                service.exchange(Money(-4, "EUR"), "USD")
            }
        }

        it("Debe lanzar excepción si la moneda origen no tiene 3 letras.") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException> {
                service.exchange(Money(5, "EURO"), "USD")
            }
        }

        it("Debe lanzar excepción si la moneda destino no tiene 3 letras.") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            shouldThrow<IllegalArgumentException> {
                service.exchange(Money(5, "EUR"), "DOLAR")
            }
        }
    }

    describe("B. Relación entre moneda origen y destino") {
        it("Debe devolver la misma cantidad si origen y destino son iguales.") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            val cantidad = 10L
            val resultado = service.exchange(Money(10, "EUR"), "EUR")

            resultado shouldBe cantidad
        }
    }

    describe("C. Estrategia de búsqueda de tasas") {
        it("Debe convertir correctamente usando una tasa directa con stub") {
            val provider = mockk<ExchangeRateProvider>()

            every { provider.rate("USDEUR") } returns 0.92

            val service = ExchangeService(provider)
            val resultado = service.exchange(Money(100, "USD"), "EUR")

            resultado shouldBe 92L
        }

        it("Debe usar spy sobre InMemoryExchangeRateProvider para verificar una llamada real correcta.") {

            val realProvider = InMemoryExchangeRateProvider(mapOf("USDEUR" to 0.92))
            val providerSpy = spyk<InMemoryExchangeRateProvider>(realProvider)
            val service = ExchangeService(providerSpy)

            service.exchange(Money(200, "EUR"), "EUR")

            verify(exactly = 0) { providerSpy.rate("EUREUR") }
        }

        it("Debe resolver una conversión cruzada cuando la tasa directa no exista usando mock"){
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider)

            every{provider.rate("EURJPY")} throws IllegalArgumentException()
            every{provider.rate("EURUSD")} returns 1.2
            every{provider.rate("USDJPY")} returns 158.0

            val resultado = service.exchange(Money(10,"EUR"),"JPY")

            resultado shouldBe 1896L
        }

        it("Debe intentar una segunda ruta intermedia si la primera falla usando mock") {
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider, supportedCurrencies = setOf("USD", "EUR"))

            every { provider.rate("GBPJPY") } throws IllegalArgumentException()
            every { provider.rate("GBPUSD") } throws IllegalArgumentException()
            every { provider.rate("USDJPY") } throws IllegalArgumentException()

            every { provider.rate("GBPEUR") } returns 1.5
            every { provider.rate("EURJPY") } returns 160.0

            val resultado = service.exchange(Money(100, "GBP"), "JPY")

            resultado shouldBe 24000L
        }

        it("Debe lanzar excepción si no existe ninguna ruta válida."){
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider, supportedCurrencies = setOf("EUR"))

            every{provider.rate("JPYEUR")} throws IllegalArgumentException()

            shouldThrow<IllegalArgumentException>{
                service.exchange(Money(300, "JPY"), "EUR")
            }
        }

        it("Debe verificar el orden exacto de las llamadas al proveedor en una conversión cruzada."){
            val provider = mockk<ExchangeRateProvider>()
            val service = ExchangeService(provider, supportedCurrencies = setOf("USD", "EUR"))

            every { provider.rate("GBPJPY") } throws IllegalArgumentException()

            every { provider.rate("GBPUSD") } throws IllegalArgumentException()
            every { provider.rate("USDJPY") } throws IllegalArgumentException()

            every { provider.rate("GBPEUR") } returns 1.15
            every { provider.rate("EURJPY") } returns 160.0

            val resultado = service.exchange(Money(100, "GBP"), "JPY")

            verifySequence {
                provider.rate("GBPJPY")
                provider.rate("GBPUSD")
                provider.rate("USDJPY")
                provider.rate("GBPEUR")
                provider.rate("EURJPY")
            }
        }

        }
    })

