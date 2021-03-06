/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.acceptance.handlers;

import org.reveno.atp.acceptance.api.commands.CreateNewAccountCommand;
import org.reveno.atp.acceptance.api.commands.NewOrderCommand;
import org.reveno.atp.acceptance.api.transactions.AcceptOrder;
import org.reveno.atp.acceptance.api.transactions.CreateAccount;
import org.reveno.atp.acceptance.api.transactions.Credit;
import org.reveno.atp.acceptance.api.transactions.Debit;
import org.reveno.atp.acceptance.model.Account;
import org.reveno.atp.acceptance.model.Order;
import org.reveno.atp.acceptance.model.Position;
import org.reveno.atp.api.commands.CommandContext;

import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

public abstract class Commands {

	public static Long createAccount(CreateNewAccountCommand cmd, CommandContext ctx) {
		long accountId = ctx.id(Account.class);
		ctx.executeTxAction(new CreateAccount(accountId, cmd.currency));
		
		if (cmd.balance > 0)
			ctx.executeTxAction(new Credit(accountId, cmd.balance, System.currentTimeMillis()));
		
		return accountId;
	}
	
	public static void credit(Credit credit, CommandContext ctx) {
		if (!ctx.repo().has(Account.class, credit.accountId))
			throw new RuntimeException();
		
		ctx.executeTxAction(credit);
	}
	
	public static void debit(Debit debit, CommandContext ctx) {
		if (!ctx.repo().has(Account.class, debit.accountId))
			throw new RuntimeException();

		ctx.executeTxAction(debit);
	}
	
	public static Long newOrder(NewOrderCommand cmd, CommandContext ctx) { 
		long orderId = ctx.id(Order.class);
		Account account = ctx.repo().get(Account.class, cmd.accountId);
		
		if (cmd.positionId != null) {
			long positionId = cmd.positionId;
			require(account.positions().positions().containsKey(positionId), format("Can't find position %d in account %d", positionId, account.id()));
			Position position = ctx.repo().get(Position.class, cmd.positionId);
			long sum = position.sum() + account.orders().stream().map(oid -> ctx.repo().getO(Order.class, oid))
					.flatMap(Commands::streamopt).filter(o -> o.positionId().isPresent() && o.positionId().get() == positionId)
					.map(Order::size).reduce(0L, Long::sum);
			require(sum != 0, format("The position %d is already filled.", positionId));
		}
		
		ctx.executeTxAction(new AcceptOrder(orderId, cmd.accountId, cmd.positionId, cmd.symbol, cmd.price, cmd.size, cmd.orderType));
		
		return orderId;
	}
	
	
	static void require(boolean value, String message) {
		if (!value)
			throw new RuntimeException(message);
	}
	
	static <T> Stream<T> streamopt(Optional<T> opt) {
	    return opt.map(Stream::of).orElseGet(Stream::empty);
	}
}
