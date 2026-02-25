import { Controller, Get, NotFoundException, Param } from '@nestjs/common';
import { PricesService } from './prices.service';
import { Price } from './interfaces/price.interface';
import { ApiParam } from '@nestjs/swagger';

@Controller('prices')
export class PricesController {
    constructor(private pricesService: PricesService) {}

    @Get(':ticker')
    @ApiParam({ name: 'ticker' })
    async getPrice(@Param('ticker') ticker: string): Promise<Price> {
        const price = await this.pricesService.getPrice(ticker);
        if (price === undefined) {
            throw new NotFoundException(
                `Price for ticker "${ticker}" not found.`,
            );
        }
        return price;
    }
}
