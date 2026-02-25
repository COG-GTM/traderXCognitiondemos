import { Module } from '@nestjs/common';
import { StocksModule } from './stocks/stocks.module';
import { PricesModule } from './prices/prices.module';
import HealthModule from './health/health.module';

@Module({
    imports: [StocksModule, PricesModule, HealthModule]
})
export class AppModule {}
