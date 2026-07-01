CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE "credentials" (
                               "id" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               "username" VARCHAR(255) UNIQUE NOT NULL,
                               "password" VARCHAR(255) NOT NULL,
                               "created_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                               "updated_at" TIMESTAMP NOT NULL DEFAULT NOW(),
                               "last_login_at" TIMESTAMP,
                               "failed_login_attempts" SMALLINT DEFAULT 0,
                               "status" BOOLEAN DEFAULT TRUE
);

CREATE TABLE "client" (
                          "client_id" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          "credential_id" UUID UNIQUE NOT NULL,
                          "name" VARCHAR(255) NOT NULL,
                          "cpf" VARCHAR(11) UNIQUE NOT NULL,
                          FOREIGN KEY ("credential_id") REFERENCES "credentials"("id")
);

CREATE TABLE "accounts" (
                            "id" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            "client_id" UUID NOT NULL,
                            "number" BIGINT UNIQUE NOT NULL,
                            "balance" DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                            FOREIGN KEY ("client_id") REFERENCES "client"("client_id")
);

CREATE TABLE "transactions" (
                                "id" UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                "origin_account_id" UUID,
                                "destination_account_id" UUID,
                                "value" DECIMAL(15, 2) NOT NULL,
                                "date" TIMESTAMP NOT NULL DEFAULT NOW(),
                                "type" VARCHAR(50) NOT NULL,
                                description VARCHAR(255),
                                "origin_transaction_id" UUID,
                                FOREIGN KEY ("origin_account_id") REFERENCES "accounts"("id"),
                                FOREIGN KEY ("destination_account_id") REFERENCES "accounts"("id"),
                                FOREIGN KEY ("origin_transaction_id") REFERENCES "transactions"("id")
);