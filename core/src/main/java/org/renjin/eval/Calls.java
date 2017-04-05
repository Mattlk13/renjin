/**
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2016 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.eval;

import org.renjin.primitives.special.ReturnException;
import org.renjin.sexp.*;

/**
 * Routines for dispatching and generally organizing function calls.
 * Much of this code is a pretty literal port of portions of eval.c and
 * object.c
 */
public class Calls {


  public static SEXP applyClosure(Closure closure, Context context, Environment callingEnvironment, FunctionCall call, PairList promisedArgs,
                                  Frame suppliedEnvironment) {

    Context functionContext = context.beginFunction(callingEnvironment, call, closure, promisedArgs);
    Environment functionEnvironment = functionContext.getEnvironment();

    try {
      matchArgumentsInto(closure.getFormals(), promisedArgs, functionContext, functionEnvironment);

      // copy supplied environment values into the function environment
      for(Symbol name : suppliedEnvironment.getSymbols()) {
        // functionEnvironment is just created and has no bindings yet, therefore we use the unsafe version of setVariable
        functionEnvironment.setVariableUnsafe(name, suppliedEnvironment.getVariable(name));
      }

      return functionContext.evaluate( closure.getBody(), functionEnvironment);

    } catch(ReturnException e) {

      if(e.getEnvironment() != functionEnvironment) {
        throw e;
      }
      return e.getValue();

    } finally {
      functionContext.exit();
    }
  }

  /* Create a promise to evaluate each argument.  Although this is most */
/* naturally attacked with a recursive algorithm, we use the iterative */
/* form below because it is does not cause growth of the pointer */
/* protection stack, and because it is a little more efficient. */

  public static PairList promiseArgs(PairList el, Context context, Environment rho)
  {
    PairList.Builder list = new PairList.Builder();

    for(PairList.Node node : el.nodes()) {

      /* If we have a ... symbol, we look to see what it is bound to.
      * If its binding is Null (i.e. zero length)
      * we just ignore it and return the cdr with all its
      * expressions promised; if it is bound to a ... list
      * of promises, we repromise all the promises and then splice
      * the list of resulting values into the return value.
      * Anything else bound to a ... symbol is an error
      */

      /* Is this double promise mechanism really needed? */

      if (node.getValue().equals(Symbols.ELLIPSES)) {
        PromisePairList dotExp = (PromisePairList)rho.findVariable(context, Symbols.ELLIPSES);
        for(PairList.Node dotNode : dotExp.nodes()) {
          list.add(dotNode.getRawTag(), dotNode.getValue());
        }
      } else if (node.getValue() == Symbol.MISSING_ARG) {
        list.add(node.getRawTag(), node.getValue());
      } else {
        if(node.getValue() instanceof Promise) {
          list.add(node.getRawTag(), node.getValue());
        } else {
          list.add(node.getRawTag(), Promise.repromise(rho, node.getValue()));
        }
      }
    }
    return list.build();
  }


  public static void matchArgumentsInto(PairList formals, PairList actuals, Context innerContext, Environment innerEnv) {
    ClosureDispatcher.matchArgumentsInto(formals, actuals, innerContext, innerEnv);
  }

  /**
   * Argument matching is done by a three-pass process:
   * <ol>
   * <li><strong>Exact matching on tags.</strong> For each named supplied argument the list of formal arguments
   *  is searched for an item whose name matches exactly. It is an error to have the same formal
   * argument match several actuals or vice versa.</li>
   *
   * <li><strong>Partial matching on tags.</strong> Each remaining named supplied argument is compared to the
   * remaining formal arguments using partial matching. If the name of the supplied argument
   * matches exactly with the first part of a formal argument then the two arguments are considered
   * to be matched. It is an error to have multiple partial matches.
   *  Notice that if f <- function(fumble, fooey) fbody, then f(f = 1, fo = 2) is illegal,
   * even though the 2nd actual argument only matches fooey. f(f = 1, fooey = 2) is legal
   * though since the second argument matches exactly and is removed from consideration for
   * partial matching. If the formal arguments contain ‘...’ then partial matching is only applied to
   * arguments that precede it.
   *
   * <li><strong>Positional matching.</strong> Any unmatched formal arguments are bound to unnamed supplied arguments,
   * in order. If there is a ‘...’ argument, it will take up the remaining arguments, tagged or not.
   * If any arguments remain unmatched an error is declared.
   *
   * @param actuals the actual arguments supplied to the list
   * @param populateMissing
   */
  public static PairList matchArguments(PairList formals, PairList actuals, boolean populateMissing) {
    return ClosureDispatcher.matchArguments(formals, actuals, populateMissing);
  }

}
