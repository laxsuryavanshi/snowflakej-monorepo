# ❄️ snowflakej-monorepo

A Java monorepo containing reusable Spring-focused libraries and shared Maven dependency management.

## 📦 Modules

| Module                 | Maven coordinates                  | What it is                                                                                                       | Docs                                             |
| ---------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| `ymsql/`               | `com.turtleby:ymsql`               | Declarative SQL + stored procedure operations defined in YAML, with optional Spring Boot auto-configuration.     | [ymsql/README.md](ymsql/README.md)               |
| `multitenancy/`        | `com.turtleby:multitenancy`        | PostgreSQL schema-based multitenancy support (tenant resolution, thread-local context, schema-aware DataSource). | [multitenancy/README.md](multitenancy/README.md) |
| `spring-dependencies/` | `com.turtleby:spring-dependencies` | Spring Boot dependency-management BOM (imports `spring-boot-dependencies`).                                      | —                                                |
| `shared-dependencies/` | `com.turtleby:shared-dependencies` | Shared Maven plugin management and build conventions (Java version, Checkstyle, Spotless, JaCoCo, etc.).         | —                                                |

## 🚀 Quick start

### Prerequisites

- Java 21+
- Maven (or use the included Maven Wrapper)

### Build everything

```bash
./mvnw clean install
```

### Run checks

```bash
# Run unit tests for all modules
./mvnw test

# Run Checkstyle
./mvnw checkstyle:check

# Apply formatting
./mvnw spotless:apply
```

### Build or test a single module

```bash
# Build only
./mvnw -pl ymsql clean test

./mvnw -pl multitenancy clean test
```

## 🧭 Repository structure

```text
.
├── shared-dependencies/     # shared build + plugin management
├── spring-dependencies/     # spring boot dependency-management BOM
├── ymsql/                   # YAML-based SQL operations library
└── multitenancy/            # PostgreSQL schema-based multitenancy library
```

## 🛠️ Development

This repo enforces consistent builds and style via:

- Maven Wrapper (`./mvnw`)
- Checkstyle (configuration at `checkstyle-config.xml`)
- Spotless (formatting)
- JaCoCo (test coverage)

## 📄 License

This project is unlicensed and for personal use.
