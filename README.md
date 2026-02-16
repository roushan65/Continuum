# Continuum

**Visual workflows that actually run.**

Start with a tiny input — a webhook, a form submission, a scheduled trigger.  
Pass it through one node. Then another. Then another.  

Each step is small.  
A little transformation.  
A filter, a branch, an enrichment, a loop.  

Nothing dramatic happens at any single node.  

But when you zoom out and look from beginning to end…  
the data has completely changed shape.  
A single event became a full report.  
A customer request became an orchestrated multi-system action.  
A spark became a living, breathing business process.

That’s **Continuum** — continuous, seamless transformation.

## Why Continuum?

Most workflow builders give you pretty diagrams that eventually break when things get real.  
Continuum is different:

- **Visual first** — built with React Flow + Eclipse Theia for a real IDE-like experience  
- **Durable execution** — powered by Temporal (fault-tolerant, long-running, scalable)  
- **Developer + non-dev friendly** — drag-drop canvas + strong typing & schema enforcement via Kotlin + Spring Boot  
- **Extensible** — add your own activities (Java/Kotlin or even JS plugins in the future)  
- **Open & safe** — Apache 2.0 licensed, patent grant included

## Current Status

- Still early (alpha / proof-of-concept stage)  
- Core drag-and-drop editor works  
- Temporal backend executes simple → parallel → join flows  
- Basic loop support is in progress  
- Node discovery & schema rendering via JSON Forms is functional  

It's rough. It's incomplete.  
But it already runs real workflows end-to-end.

## Come play

If you hate n8n at scale, if Temporal feels too code-heavy, if you want visual workflows that don't lie to you —  
fork it, break it, fix it, add nodes, yell at me.

No gatekeeping. No judgment.

Welcome to the river.  
Let's make it flow.
