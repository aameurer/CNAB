
import os
import glob
from src.parser import CNAB240Parser
from src.db import DatabaseManager
from src.comparator import Comparator

def main():
    # Configuration
    api_files_dir = r'c:\Compara Retorno\Lotes API dia 12'
    general_file_pattern = r'c:\Compara Retorno\Dia 12*.RET'
    
    # Initialize
    db = DatabaseManager()
    parser = CNAB240Parser()
    
    print("Iniciando processamento...")
    
    # 1. Process API Files
    print(f"Lendo arquivos da API em: {api_files_dir}")
    api_files = glob.glob(os.path.join(api_files_dir, '*.RET'))
    
    all_api_transactions = []
    for file_path in api_files:
        print(f"  Processando: {os.path.basename(file_path)}")
        transactions = parser.parse_file(file_path)
        all_api_transactions.extend(transactions)
        
    print(f"Salvando {len(all_api_transactions)} transações da API no banco de dados...")
    db.save_transactions(all_api_transactions, 'api_transactions')
    
    # 2. Process General File
    print(f"Procurando arquivo geral...")
    general_files = glob.glob(general_file_pattern)
    if not general_files:
        print("Erro: Arquivo geral não encontrado.")
        return
    
    # Assuming only one general file matches, or take the first one
    general_file_path = general_files[0]
    print(f"Processando arquivo geral: {os.path.basename(general_file_path)}")
    
    general_transactions = parser.parse_file(general_file_path)
    print(f"Salvando {len(general_transactions)} transações do arquivo geral no banco de dados...")
    db.save_transactions(general_transactions, 'geral_transactions')
    
    # 3. Compare
    print("Iniciando comparação...")
    comparator = Comparator(db)
    results = comparator.compare_transactions()
    
    # 4. Report
    print("Gerando relatórios...")
    comparator.generate_report(results)
    
    db.close()
    print("Concluído!")

if __name__ == "__main__":
    main()
