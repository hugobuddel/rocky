/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rockstar.debugger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import rockstar.expression.Expression;
import rockstar.parser.ExpressionParser;
import rockstar.parser.Line;
import rockstar.runtime.BlockContext;
import rockstar.runtime.BlockContextListener;
import rockstar.runtime.Value;
import rockstar.statement.Statement;

/**
 *
 * @author Gabor
 */
public class DebugListener implements BlockContextListener {

    private final Map<String, String> options;

    private boolean evalMode = false;

    DebugListener(Map<String, String> options) {
        this.options = options;
    }

    @Override
    public void beforeStatement(BlockContext ctx, Statement stmt) {
        if (!evalMode) {
            atStatement(ctx, stmt);
        }
    }

    @Override
    public void beforeExpression(BlockContext ctx, Expression exp) {
        if (!evalMode) {
        }
    }

    @Override
    public void afterExpression(BlockContext ctx, Expression exp, Value v) {
        if (!evalMode) {
        }
    }

    private final List<String> watches = new LinkedList<>();

    private void atStatement(BlockContext ctx, Statement stmt) {

        if (stopAtStetement(ctx, stmt)) {
            Line l = stmt.getLine();

            System.out.format("%4d %s\n", l.getLnum(), l.getOrigLine());
            for (int i = 0; i < watches.size(); i++) {
                String varName = watches.get(i);
                Value value = ctx.getVariableValue(varName);
                System.out.format("Watch #%d: %s = %s\n", i + 1, varName, value.toString());
            }

            boolean continueRun = false;
            while (!continueRun) {
                System.out.print(ctx.getName() + "> ");
                try {
                    String line = ctx.getInput().readLine();

                    if (line.equals("5") || line.equals("")) {
                        // step into
                        stepInto = true;
                        continueRun = true;
                    } else if (line.equals("6")) {
                        // steo over
                        // stop at the next statement in the same context
                        stepOvers.push(ctx);
                        continueRun = true;
                    } else if (line.equals("7")) {
                        // step return
                        BlockContext parent = ctx.getParent();
                        if (parent != null) {
                            stepOvers.push(parent);
                        }
                        continueRun = true;
                    } else if (line.equals("8")) {
                        // step run
                        continueRun = true;
                    } else if (line.startsWith("s ")) {
                        // show variable
                        String varName = line.substring(2).trim();
                        Value value = ctx.getVariableValue(varName);
                        System.out.format("%s = %s\n", varName, value.toString());
                    } else if (line.startsWith("w ")) {
                        // add watch
                        String varName = line.substring(2).trim();
                        watches.add(varName);
                        Value value = ctx.getVariableValue(varName);
                        System.out.format("Watch #%d: %s = %s\n", watches.size(), varName, value.toString());
                    } else if (line.startsWith("wr ")) {
                        // remove watch
                        String varName = line.substring(3).trim();
                        if (varName.startsWith("#")) {
                            try {
                                int idx = Integer.parseInt(varName.substring(1));
                                varName = watches.get(idx - 1);
                            } catch (NumberFormatException nfe) {
                                // fall back to exact string match
                            }
                        }
                        if (watches.remove(varName)) {
                            System.out.format("Watch removed: %s\n", varName);
                        } else {
                            System.out.println("Unknown watch");
                        }
                    } else if (line.startsWith("br")) {
                        // remove breakpoint
                        String lineNum = line.substring(2).trim();
                        if (lineNum.equals("")) {
                            // default: current line
                            lineNum = Integer.toString(l.getLnum());
                        }
                        if (breakpoints.remove(lineNum)) {
                            System.out.format("Breakpoint removed: %s\n", lineNum);
                        } else {
                            System.out.println("Unknown breakpoint");
                        }
                    } else if (line.startsWith("b")) {
                        // add breakpoint
                        String lineNum = line.substring(1).trim();
                        if (lineNum.equals("")) {
                            // default: current line
                            lineNum = Integer.toString(l.getLnum());
                        } else {
                            try {
                                Integer.parseInt(lineNum);
                            } catch (NumberFormatException ex) {
                                lineNum = null;
                            }
                        }
                        if (lineNum != null) {
                            breakpoints.add(lineNum);
                            System.out.format("Breakpoint added at line %s\n", lineNum);
                        } else {
                            System.out.println("Wrong line number");
                        }
                    } else if (line.startsWith("?")) {
                        // add breakpoint
                        String helpCmd = line.substring(1).trim();
                        RockstarDebugger.printDebuggerHelp(helpCmd);
                    } else {
                        System.out.println("Wrong command, use '?' for command help");
                    }
                } catch (IOException ex) {
                    continueRun = true;
                }
            }
        }
    }

    private boolean stepInto = true;
    private final Stack<BlockContext> stepOvers = new Stack<>();
    private final List<String> breakpoints = new LinkedList<>();

    private boolean stopAtStetement(BlockContext ctx, Statement stmt) {
        // stop at each statement if stepInto mode
        if (stepInto) {
            stepInto = false;
            return true;
        }
        // stop at the next statement of a specific context
        if (stepOvers.size() > 0 && stepOvers.peek() == ctx) {
            stepOvers.pop();
            return true;
        }
        // stop at a line number
        if (!breakpoints.isEmpty() && breakpoints.contains(Integer.toString(stmt.getLine().getLnum()))) {
            return true;
        }
        return false;
    }

}