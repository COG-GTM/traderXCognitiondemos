export interface Position {
	accountid: number;
	quantity: number;
	security: string;
	updated: Date;
}

export interface Account {
	id: number;
	displayName?: string;
}
