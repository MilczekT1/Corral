# How it fits together

```mermaid
flowchart LR
    subgraph consumer["Your repo"]
        CT["ProjectArchitectureTest<br/><i>@AnalyzeClasses</i>"]
        PRG["ProjectRulesGroup<br/><i>your catalog root</i>"]
        STORE[("archunit/frozen<br/><i>committed</i>")]
    end

    subgraph rules["corral-rules"]
        TG["TestingRulesGroup<br/><i>group</i>"]
        LG["LoggingRulesGroup<br/><i>group</i>"]
        R1["TestClassNamingConventionRule"]
        R2["NoMockedRepositoryInIntegrationTestRule"]
        R3["NoSystemOutRule"]
        R4["NoSystemErrRule"]
    end

    subgraph sdk["corral-sdk<br/><i>framework</i>"]
        DR["DocumentedRule"]
        FMT["AgentFriendlyFailureDisplayFormat"]
    end

    CT -->|"ArchTests.in(...)"| PRG
    PRG --> TG
    PRG --> LG
    TG --> R1
    TG --> R2
    LG --> R3
    LG --> R4
    R1 & R2 & R3 & R4 -.->|"implements"| DR
    R1 & R2 & R3 & R4 -.->|"violation"| FMT
    FMT -.->|"WHY / HOW TO FIX"| OUT["Build output"]
    R1 & R2 & R3 & R4 <-->|"known violations"| STORE
```

Each arrow from a group is an `@ArchTest ArchTests` field; each leaf is an `@ArchTest ArchRule`.
Because `ArchTests.in(X)` descends into `X`'s `@ArchTest` fields, the same shape nests indefinitely —
`ProjectRulesGroup` is just a group whose members happen to be groups, and it lives in your repo.

The two jars split by role: `corral-sdk` is the framework for authoring rules, `corral-rules`
is the catalog of rules built on it. Depending on the catalog pulls the framework in transitively;
depend on the SDK alone to write your own rules without adopting these.

See also **[CONTRIBUTING.md § The shape](../CONTRIBUTING.md#the-shape)** for the authoring-side view:
one rule one class, how membership is declared, and why the module boundary enforces the direction.

> The class names above are illustrative. The [rules catalog](rules.md) is the current list.
