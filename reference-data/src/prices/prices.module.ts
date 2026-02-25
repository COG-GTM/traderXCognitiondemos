import { Module } from '@nestjs/common';
import { PricesController } from './prices.controller';
import { PricesService } from './prices.service';
import { StocksModule } from '../stocks/stocks.module';

@Module({
    imports: [StocksModule],
    providers: [PricesService],
    controllers: [PricesController],
})
export class PricesModule {}
