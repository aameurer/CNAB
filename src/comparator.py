import csv
import os
from datetime import datetime

class Comparator:
    def __init__(self, db_manager):
        self.db = db_manager

    def get_all_transactions(self, table_name):
        return self.db.get_all_transactions(table_name)

    def compare_transactions(self, date_field='data_ocorrencia', start_date=None, end_date=None):
        # Load data (list of dicts)
        api_data = self.get_all_transactions('api_transactions')
        geral_data = self.get_all_transactions('geral_transactions')
        
        if not api_data and not geral_data:
            print("Nenhum dado encontrado para comparar.")
            return {}
            
        print(f"Filtrando registros com valor pago > 0 para comparação...")
        api_data = [t for t in api_data if t.get('valor_pago', 0) > 0]
        geral_data = [t for t in geral_data if t.get('valor_pago', 0) > 0]
        
        # Filter by Date Range
        if start_date and end_date:
            print(f"Filtrando por {date_field} entre {start_date} e {end_date}...")
            
            def parse_date(date_str):
                try:
                    return datetime.strptime(date_str, '%d%m%Y')
                except (ValueError, TypeError):
                    return None

            try:
                start_dt = datetime.strptime(start_date, '%d/%m/%Y')
                end_dt = datetime.strptime(end_date, '%d/%m/%Y')
                
                def filter_date(data):
                    filtered = []
                    for t in data:
                        dt = parse_date(t.get(date_field))
                        if dt and start_dt <= dt <= end_dt:
                            filtered.append(t)
                    return filtered

                api_data = filter_date(api_data)
                geral_data = filter_date(geral_data)
            except Exception as e:
                print(f"Erro ao filtrar datas: {e}")

        # Deduplicate
        print("Removendo duplicatas...")
        def deduplicate(data):
            seen = set()
            unique_data = []
            for t in data:
                # Key: nosso_numero, valor_pago, data_ocorrencia
                key = (t.get('nosso_numero'), t.get('valor_pago'), t.get('data_ocorrencia'))
                if key not in seen:
                    seen.add(key)
                    unique_data.append(t)
            return unique_data

        api_data = deduplicate(api_data)
        geral_data = deduplicate(geral_data)
        
        # Prepare lookup maps
        # Ensure nosso_numero is stripped string
        for t in api_data:
            t['nosso_numero'] = str(t.get('nosso_numero', '')).strip()
        for t in geral_data:
            t['nosso_numero'] = str(t.get('nosso_numero', '')).strip()
            
        # Round monetary values
        monetary_cols = ['valor_pago', 'valor_titulo', 'valor_liquido', 'juros_multa', 'desconto', 'abatimento', 'iof', 'outras_despesas', 'outros_creditos']
        for t in api_data + geral_data:
            for col in monetary_cols:
                if col in t:
                    try:
                        t[col] = round(float(t[col]), 2)
                    except (ValueError, TypeError):
                        pass

        # Index by nosso_numero
        api_map = {}
        for t in api_data:
            nn = t['nosso_numero']
            if nn not in api_map: api_map[nn] = []
            api_map[nn].append(t)
            
        geral_map = {}
        for t in geral_data:
            nn = t['nosso_numero']
            if nn not in geral_map: geral_map[nn] = []
            geral_map[nn].append(t)
            
        # Merge keys
        all_keys = set(api_map.keys()) | set(geral_map.keys())
        
        missing_in_geral = []
        missing_in_api = []
        matched = []
        discrepancies = []
        
        for nn in all_keys:
            api_list = api_map.get(nn, [])
            geral_list = geral_map.get(nn, [])
            
            if not geral_list:
                # All in API are missing in Geral
                for item in api_list:
                    new_item = {f"{k}_api": v for k, v in item.items()}
                    new_item['nosso_numero'] = nn
                    missing_in_geral.append(new_item)
            elif not api_list:
                # All in Geral are missing in API
                for item in geral_list:
                    new_item = {f"{k}_geral": v for k, v in item.items()}
                    new_item['nosso_numero'] = nn
                    missing_in_api.append(new_item)
            else:
                # Present in both. Cartesian product.
                for a_item in api_list:
                    for g_item in geral_list:
                        # Create merged item
                        merged_item = {'nosso_numero': nn}
                        for k, v in a_item.items():
                            if k != 'nosso_numero': merged_item[f"{k}_api"] = v
                        for k, v in g_item.items():
                            if k != 'nosso_numero': merged_item[f"{k}_geral"] = v
                        
                        matched.append(merged_item)
                        
                        # Check discrepancies
                        val_api = a_item.get('valor_pago', 0)
                        val_geral = g_item.get('valor_pago', 0)
                        diff = abs(val_api - val_geral)
                        
                        date_api = a_item.get(date_field)
                        date_geral = g_item.get(date_field)
                        
                        if diff > 0.01 or date_api != date_geral:
                            merged_item['diff_valor_pago'] = diff
                            discrepancies.append(merged_item)

        return {
            'missing_in_geral': missing_in_geral,
            'missing_in_api': missing_in_api,
            'discrepancies': discrepancies,
            'matched': matched
        }

    def generate_report(self, results, output_dir='reports'):
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
            
        if not results:
            print("Sem resultados para gerar relatório.")
            return

        # Summary
        summary = []
        summary.append("Relatório de Comparação CNAB240")
        summary.append("===============================")
        summary.append(f"Registros encontrados somente na API: {len(results['missing_in_geral'])}")
        summary.append(f"Registros encontrados somente no Arquivo Geral: {len(results['missing_in_api'])}")
        summary.append(f"Registros correspondentes (Total): {len(results['matched'])}")
        summary.append(f"  - Com divergências (Valor/Data): {len(results['discrepancies'])}")
        summary.append(f"  - Sem divergências: {len(results['matched']) - len(results['discrepancies'])}")
        
        with open(f'{output_dir}/resumo.txt', 'w', encoding='utf-8') as f:
            f.write('\n'.join(summary))
            
        # CSV Reports
        def write_csv(filename, data):
            if not data:
                return
            # Use keys from first item
            keys = list(data[0].keys())
            try:
                with open(os.path.join(output_dir, filename), 'w', newline='', encoding='utf-8') as f:
                    writer = csv.DictWriter(f, fieldnames=keys)
                    writer.writeheader()
                    writer.writerows(data)
                print(f"Gerado: {filename}")
            except PermissionError:
                print(f"Erro: O arquivo '{filename}' está aberto. Feche-o e tente novamente.")

        write_csv('divergencias.csv', results['discrepancies'])
        write_csv('sobra_api.csv', results['missing_in_geral'])
        write_csv('sobra_geral.csv', results['missing_in_api'])
        write_csv('correspondentes.csv', results['matched'])
        
        print(f"Relatórios (CSV) gerados em '{output_dir}'")
