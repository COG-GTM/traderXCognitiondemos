# Objection handling — internal only

Not for the customer deck. Answers are anchored to what this demo actually shows; anything
we cannot back is marked as such, because in a room that has already said the other proof of
concept "has some ways to go to prove value", a bluff costs more than a "let me come back to you".

## The three hardest questions in this room

**1. "We're scoping build vs. buy. What stops us building this ourselves in six months?"**

The agent is the visible part and the least defensible part. What is hard to build is the
context layer underneath it — the indexed understanding, the persistent knowledge that
accrues per repo, the review model trained on it, and the sandbox fleet. You saw all four in
40 minutes on a repo neither of us owns. A build programme starts that clock at zero, and the
clock is the asset: day one is ~40% effective, day thirty is a developer with the institutional
knowledge of someone who has been there years. Building it yourself means paying for the
learning curve twice — once to build the platform, once to accumulate the knowledge.
*Discovery question back:* "If you did build it, which team owns it in year two?"

**2. "Where does our code go, and what can the agent touch?"**

Each session runs in an ephemeral Firecracker micro-VM on AWS, fully sandboxed, with its own
shell, browser and IDE, torn down after. Access is what you grant it — for EDP that is a
cloned GitLab instance, with approvals in progress. Everything an agent did is auditable at
the session level. *Trap to avoid:* do not promise on-prem or a specific region/retention
posture on the call — that is a follow-up with our security team, and the SOC 2 / pen test
material goes over properly rather than being paraphrased live.

**3. "Your other Barclays proof of concept hasn't proved value yet. Why is this different?"**

Take it straight: EDP is a migration on a hard estate, and migrations prove value at the end,
not in the middle. Feature work proves value every week, because the unit is a merged PR
against a real backlog. That is exactly why today was a feature and not a migration. The
proposal is a thirty-day run against a boring, regulatory-driven backlog where the measure is
PRs merged and review time saved — reported weekly, not at the end.

## The rest

**"How is this different from Copilot? We already have it."**
Copilot makes your developer faster at the keyboard. Every agent in that tier still needs the
developer sitting there, approving each step, on their machine. Nothing in the last 40 minutes
ran on a laptop and nothing asked permission mid-task. Those are different products solving
different bottlenecks — and the bottleneck in an IB backlog is not typing speed.

**"What about code quality? We can't have an agent degrading a trading system."**
Everything lands as a PR and review is mandatory — no output skips human review regardless of
agent confidence. On top of that, automated review triages the volume. And note what the demo
showed: on the deliberately bad ticket, it refused. The failure mode people fear is an agent
that confidently builds the wrong thing; the control is that ambiguity escalates.

**"Our codebase is 20 years old, undocumented, and half of it is COBOL-adjacent."**
That is the best fit, not the worst. Large-scale brownfield work sitting on backlogs is the
sweet spot. TraderX is deliberately polyglot for this reason, but it is small — if they want
the hard version, offer the next session on their own repo.

**"What happens when it gets something wrong?"**
It shows up in review, like a junior's work, except it never gets tired at PR forty. Be
willing to show a real recovery in the demo rather than only the happy path.

**"How do we measure this? I need a number for the exec committee."**
Resist inventing one. The measurable set from a thirty-day run: PRs merged per week against
the target backlog, human review time per PR, cycle time from ticket to merge, and the
proportion of tickets that needed a human to re-scope. Offer to instrument these from day one
rather than quoting a benchmark from another customer's estate.

**"Who's accountable when an agent's code causes an incident?"**
The engineer who merged it. The agent is never accountable in the RACI — that is the design,
not a caveat. Point at the table in `ROLE_CHANGES.md`.

**"Does it work outside a small high-performing team?"**
His actual framing, so answer it head-on: that is what the fan-out beat was. Four concurrent
sessions, one dependency-queued, all driven off written tickets rather than tribal knowledge.
The constraint on scale is the quality of the backlog, which is a management problem the bank
already knows how to solve — not an agent problem.

## Claims to keep hedged

* "~40% on day one, 15–20 years of institutional knowledge by day thirty" — this is our
  narrative framing, not a benchmark. Say it as framing.
* Anything about specific Barclays approvals status beyond what Paul Sampat's team has said.
* Any comparison to a named competitor's internals we have not verified.
