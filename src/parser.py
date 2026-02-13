
import os

class CNAB240Parser:
    def parse_file(self, file_path):
        transactions = []
        current_transaction = {}
        
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            for line in f:
                if len(line) < 240:
                    continue
                
                segment_type = line[13]
                
                if segment_type == 'T':
                    # Start of a new transaction
                    current_transaction = {
                        'banco': line[0:3],
                        'lote': line[3:7],
                        'tipo_registro': line[7],
                        'n_seq': line[8:13],
                        'segmento': 'T',
                        'movimento': line[15:17],
                        'agencia': line[17:22],
                        'conta': line[23:35],
                        'nosso_numero': line[37:57].strip(),
                        'carteira': line[57],
                        'numero_documento': line[58:73].strip(),
                        'vencimento': line[73:81],
                        'valor_titulo': float(line[81:96]) / 100,
                        'banco_cobrador': line[96:99],
                        'agencia_cobradora': line[99:104],
                        'id_titulo_empresa': line[105:130].strip(),
                        'tipo_inscricao': line[131],
                        'num_inscricao': line[132:147],
                        'nome_pagador': line[147:187].strip(),
                        'num_contrato': line[187:197],
                        'valor_tarifa': float(line[197:212]) / 100,
                        'motivo_ocorrencia': line[212:222].strip(),
                        'file_source': os.path.basename(file_path)
                    }
                elif segment_type == 'U':
                    if current_transaction and current_transaction.get('n_seq') == str(int(line[8:13]) - 1).zfill(5):
                        # Matching U segment for the previous T segment (usually sequential)
                        # Or just assuming strict T -> U ordering
                        pass
                    
                    # Update current transaction with U data
                    # Note: We assume U follows T. If not, we might need better logic.
                    # Given the sample, T is followed by U.
                    
                    current_transaction.update({
                        'juros_multa': float(line[17:32]) / 100,
                        'desconto': float(line[32:47]) / 100,
                        'abatimento': float(line[47:62]) / 100,
                        'iof': float(line[62:77]) / 100,
                        'valor_pago': float(line[77:92]) / 100,
                        'valor_liquido': float(line[92:107]) / 100,
                        'outras_despesas': float(line[107:122]) / 100,
                        'outros_creditos': float(line[122:137]) / 100,
                        'data_ocorrencia': line[137:145],
                        'data_credito': line[145:153],
                    })
                    
                    # Store the complete transaction
                    transactions.append(current_transaction)
                    current_transaction = {} # Reset
                else:
                    # Header or Trailer or other segments
                    continue
                    
        return transactions

