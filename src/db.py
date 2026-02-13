
import sqlite3
import pandas as pd

class DatabaseManager:
    def __init__(self, db_name='transactions.db'):
        self.conn = sqlite3.connect(db_name)
        self.create_tables()

    def create_tables(self):
        cursor = self.conn.cursor()
        
        # Common schema for both tables
        schema = """
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            banco TEXT,
            lote TEXT,
            tipo_registro TEXT,
            n_seq TEXT,
            segmento TEXT,
            movimento TEXT,
            agencia TEXT,
            conta TEXT,
            nosso_numero TEXT,
            carteira TEXT,
            numero_documento TEXT,
            vencimento TEXT,
            valor_titulo REAL,
            banco_cobrador TEXT,
            agencia_cobradora TEXT,
            id_titulo_empresa TEXT,
            tipo_inscricao TEXT,
            num_inscricao TEXT,
            nome_pagador TEXT,
            num_contrato TEXT,
            valor_tarifa REAL,
            motivo_ocorrencia TEXT,
            juros_multa REAL,
            desconto REAL,
            abatimento REAL,
            iof REAL,
            valor_pago REAL,
            valor_liquido REAL,
            outras_despesas REAL,
            outros_creditos REAL,
            data_ocorrencia TEXT,
            data_credito TEXT,
            file_source TEXT
        """
        
        cursor.execute(f"CREATE TABLE IF NOT EXISTS api_transactions ({schema})")
        cursor.execute(f"CREATE TABLE IF NOT EXISTS geral_transactions ({schema})")
        self.conn.commit()

    def save_transactions(self, transactions, table_name):
        if not transactions:
            return
        
        df = pd.DataFrame(transactions)
        df.to_sql(table_name, self.conn, if_exists='append', index=False)

    def get_all_transactions(self, table_name):
        return pd.read_sql(f"SELECT * FROM {table_name}", self.conn)

    def clear_tables(self):
        cursor = self.conn.cursor()
        cursor.execute("DELETE FROM api_transactions")
        cursor.execute("DELETE FROM geral_transactions")
        self.conn.commit()

    def close(self):
        self.conn.close()
