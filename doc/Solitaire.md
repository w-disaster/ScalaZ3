# Solitaire

I'd like to propose another way to carry out the Solitaire exercise of the lab06, where the problem is modeled using a [SAT](https://en.wikipedia.org/wiki/Boolean_satisfiability_problem) approach i.e. as a boolean satisfiability problem.

Given a propositional formula, i.e. a set of boolean variables and a set of constraints (AND $\land$, OR $\lor$, NOT $\lnot$ and parenthesis), it is said to be SATisfiable if exists at least one assignment to the variables which respects all the constraints.

In particular, in this exercise the [SMT](https://en.wikipedia.org/wiki/Satisfiability_modulo_theories) (Satisfability Modulo Theorem) has been exploited, which is a generalization of the SAT where also integer, real number and more complex formulas can be used.

In the literature, there are a lot of SAT/SMT solvers, most of them implemented using the DPLL alorithm, as for example [z3](https://github.com/Z3Prover/z3), written in C++.


In Scala there aren't much repos available but I found this one which is a DSL for constraint solving with z3 

### Applications

This solvers have a lot of applications including:
- Model checking to verify the logical formulas;
- Hardware verification, scheduling, planning etc. 
- In security, in Return-Oriented Programming ([ROP](https://github.com/sashs/Ropper)) to find appropriate gadgets in a binary file; to attack [logic locking](https://link.springer.com/book/10.1007/978-3-030-15334-2) mechanisms inside ICs also.



## Modeling

The grid of the Solitaire is implemented as a matrix of width $w$ and height $h$, where each cell $c_{i, j}$ is a symbolic integer variable $\texttt{IntVar}$ which expresses its marking number. This type of variable is given by the library and it's used by the solver which explores its possible assignments.

### Constraints

- Bound constraint: each cell of the grid can assume a value between $1$ and $w * h$ (included):
    
    $$\texttt{boundConstraint($c$)} :=  \bigwedge\limits_{i=0}^h \bigwedge\limits_{j=0}^w (c_{i, j} > 0 \land c_{i, j} \leq w * h)$$

- The integers assigned to the cells must be all different. Let $c'$ be the matrix $c$ flattened. The constraint can be expressed as:

    $$\texttt{allDiffConstraint($c'$)} := \bigwedge \limits_{0 \leq i < j < w*h} c'_i \neq  c'_j$$

- The central position of the grid has always value equal to $1$ because it's the first placing:

    $$\texttt{firstPlacingConstraint($c$)} :=  c_{h / 2, w/2} = 1$$

- Previous marking constraint. If the position $(i,j)$ has value equal to $k$, there must be exactly one position in the possible previous marking positions with value $k-1$.

    NB: the previous possible marking offsets are equal to the next possible ones. Given the position $(i, j)$, the previous marking could've been done in one of $P_{i,j}$ cell.

    $P_{i,j} = \{(i+3,j), (i-3, j), (i, j+3), (i, j-3), (i+2,j+2), (i+2,j-2), (i-2,j+2), (i-2,j-2)\}$

    This constraint holds for all the cells, with the exception of the central one where the first placing is done: there's no previous marking.

    The exactly one encoding can be expressed as a logic and of $\texttt{atMostOne}$ and $\texttt{atLeastOne}$:

    $$\texttt{exactlyOne} := \texttt{atMostOne} \land \texttt{atLeastOne}$$
    
    - The $\texttt{atLeastOne}$ of a set of boolean variables $v$ can be expressed as a logical Or:
    
        $$\texttt{atLeastOne}(v) := \bigvee\limits_{i=0}^{n} v_i$$

    - The $\texttt{atMostOne}$ using a pairwise encoding (any combinations of two variables cannot be true at the same time)

        $$\texttt{atMostOne}(v) := \bigwedge \limits_{0 \leq i < j < w*h} \lnot(v_i \land v_j)$$
    
    
        Anyway, in this implementation I used the Heule Encoding (one of many available in literature), which can reduce, with the respect of the pairwise one, the number of clauses to $O(n)$ introducing new $O(n)$ variables.

    Summarizing, it must hold:
    $$\texttt{prevConstraint($c$)} :=  \bigwedge\limits_{i=0}^h \bigwedge\limits_{j=0}^w \texttt{exactlyOne($P_{i,j}(c)$)}$$
    
    where $i \neq h/2, j \neq w/2$.

    
### $\texttt{atMostOne}$: Heule encoding

Split the constraints using additional variables.

- When $n \leq 4$, apply the pairwise encoding, using at most $6$ clauses.
- When $n > 4$:
    - introduce a new boolean variable $y$
    - $\texttt{atMostOne}([v_1, v_2, v_3, y]) \land \texttt{atMostOne}([\lnot y, v_4, \dots, v_n])$

        Encode the second one recursevely.



